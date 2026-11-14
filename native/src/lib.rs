mod core;
use jni::{JNIEnv, objects::{JClass, JString}, sys::jstring};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_moviebox_mobile_Engine_call(mut env: JNIEnv, _: JClass, input: JString, path: JString) -> jstring {
    let result = (|| -> Result<String, String> {
        let text: String = env.get_string(&input).map_err(|e| e.to_string())?.into();
        let path: String = env.get_string(&path).map_err(|e| e.to_string())?.into();
        Ok(core::request(&text, &path))
    })();
    let output = result.unwrap_or_else(|error| serde_json::json!({"ok":false,"error":error}).to_string());
    env.new_string(output).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}