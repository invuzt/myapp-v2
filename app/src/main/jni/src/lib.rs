use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[no_mangle]
pub extern "system" fn Java_com_odfiz_timemark_EditorActivity_helloFromRust(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = env.new_string("Odfiz Engine: Rust Aktif!").expect("Gagal buat string");
    output.into_raw()
}

