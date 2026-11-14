#[path = "../../../native/src/core.rs"]
mod core;
use std::ffi::{CStr, CString, c_char};

/// Inputs must be valid, NUL-terminated UTF-8, alive for the duration of this call.
/// The caller owns the returned string and must release it exactly once with cubox_free.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cubox_request(input: *const c_char, path: *const c_char) -> *mut c_char {
    let output = std::panic::catch_unwind(|| {
        if input.is_null() || path.is_null() {
            return "{\"ok\":false,\"error\":\"Missing engine input\"}".to_string();
        }
        let input = unsafe { CStr::from_ptr(input) }.to_str();
        let path = unsafe { CStr::from_ptr(path) }.to_str();
        match (input, path) {
            (Ok(input), Ok(path)) => core::request(input, path),
            _ => "{\"ok\":false,\"error\":\"Invalid UTF-8 input\"}".to_string(),
        }
    }).unwrap_or_else(|_| "{\"ok\":false,\"error\":\"Engine failed\"}".to_string());
    CString::new(output).map(CString::into_raw).unwrap_or(std::ptr::null_mut())
}

/// Accepts only null or a pointer previously returned by cubox_request, once.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn cubox_free(value: *mut c_char) {
    if !value.is_null() { drop(unsafe { CString::from_raw(value) }); }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn null_arguments_return_owned_json_error() {
        unsafe {
            let result = cubox_request(std::ptr::null(), std::ptr::null());
            let json: serde_json::Value = serde_json::from_str(CStr::from_ptr(result).to_str().unwrap()).unwrap();
            assert_eq!(json["ok"], false);
            cubox_free(result);
            cubox_free(std::ptr::null_mut());
        }
    }
}
