use moviebox_tui::{service::MovieBoxService, providers::{ProviderKind, ReleaseProvider, Release, PlaybackSource, ResolutionIntent}};
use serde_json::{Value, json};
use std::sync::{OnceLock, Mutex};

static ENGINE: OnceLock<(tokio::runtime::Runtime, MovieBoxService)> = OnceLock::new();
static INIT: Mutex<()> = Mutex::new(());

fn init(path: &str) -> Result<(), String> {
    let _guard = INIT.lock().map_err(|_| "Engine initialization failed")?;
    if ENGINE.get().is_some() { return Ok(()); }
    // Set app-private paths before starting Rust worker threads.
    unsafe {
        std::env::set_var("MOVIEBOX_CONFIG_DIR", format!("{path}/config"));
        std::env::set_var("MOVIEBOX_DATA_DIR", format!("{path}/data"));
        std::env::set_var("MOVIEBOX_CACHE_DIR", format!("{path}/cache"));
    }
    let rt = tokio::runtime::Builder::new_multi_thread().worker_threads(3).enable_all().build().map_err(|e| e.to_string())?;
    let service = { let _enter = rt.enter(); MovieBoxService::new() };
    ENGINE.set((rt, service)).map_err(|_| "Already initialized".to_string())
}

async fn execute(s: &MovieBoxService, v: Value) -> Result<Value, String> {
    let op = v["op"].as_str().unwrap_or("");
    let provider = ProviderKind::parse(v["provider"].as_str().unwrap_or("moviebox")).ok_or("Unknown provider")?;
    let id = v["id"].as_str().unwrap_or("");
    let se = v["season"].as_u64().unwrap_or(0) as usize;
    let ep = v["episode"].as_u64().unwrap_or(0) as usize;
    match op {
        "home" => s.homepage("", v["page"].as_u64().unwrap_or(1) as usize).await.map(|(items, _)| json!(items)),
        "search" => s.search_typed(provider, v["query"].as_str().unwrap_or(""), v["page"].as_u64().unwrap_or(1) as usize).await.map(|items| json!(items)).map_err(|e| e.user_message(provider)),
        "details" => s.details_typed(provider, id).await.map(|x| json!(x)).map_err(|e| e.user_message(provider)),
        "releases" => {
            let result = match provider {
                ProviderKind::MovieBox => s.client.episode_streams(id, se, ep).await,
                ProviderKind::FourKHdHub => ReleaseProvider::episode_streams(s.fourk_client.as_ref().ok_or("4KHDHub unavailable")?, id, se, ep).await,
                ProviderKind::Dramachi => ReleaseProvider::episode_streams(&s.dramachi_client, id, se, ep).await,
                ProviderKind::BdixCircleFtp => s.circleftp_client.episode_streams(id, se, ep).await,
                ProviderKind::BdixDhakaFlix => s.dhakaflix_client.episode_streams(id, se, ep).await,
                ProviderKind::Addons => return Err("Add-on configuration is not enabled in this mobile version".into()),
            };
            result.map(|x| json!(x)).map_err(|e| e.user_message(provider))
        },
        "resolve" => {
            let mut release: Release = serde_json::from_value(v["release"].clone()).map_err(|e| e.to_string())?;
            let index = v["mirror"].as_u64().unwrap_or(0) as usize;
            let mirror = release.mirrors.get(index).cloned().ok_or("Server unavailable")?;
            release.mirrors = vec![mirror.clone()];
            let source = if release.provider == ProviderKind::FourKHdHub {
                let intent = if v["download"].as_bool().unwrap_or(false) { ResolutionIntent::Download } else { ResolutionIntent::Playback };
                s.fourk_client.as_ref().ok_or("4KHDHub unavailable")?.resolve_release(&release, intent).await.map_err(|e| e.to_string())?
            } else {
                PlaybackSource { provider: release.provider, url: mirror.resolver_url, headers: mirror.headers, subtitle: None, source_label: mirror.label, max_height: Some(release.resolution_u64()) }
            };
            Ok(json!(source))
        },
        "captions" => s.get_ext_captions(id, v["resource_id"].as_str().unwrap_or(""), &[], se, ep).await.map(|x| json!(x)),
        _ => Err("Unknown engine request".into())
    }
}


pub fn request(text: &str, path: &str) -> String {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<Value, String> {
        init(path)?;
        let (rt, service) = ENGINE.get().ok_or("Engine unavailable")?;
        let v = serde_json::from_str(text).map_err(|e| e.to_string())?;
        rt.block_on(async { tokio::time::timeout(std::time::Duration::from_secs(75), execute(service, v)).await.map_err(|_| "Provider timed out. Try another server.".to_string())? })
    })).unwrap_or_else(|_| Err("Engine encountered an unexpected error".into()));
    match result {
        Ok(value) => json!({"ok":true,"data":value}).to_string(),
        Err(error) => json!({"ok":false,"error":error}).to_string(),
    }
}