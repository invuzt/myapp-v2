use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jstring;

#[no_mangle]
pub extern "system" fn Java_com_odfiz_timemark_EditorActivity_helloFromRust(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = env.new_string("Odfiz Engine: Mesin Rust Siap!").expect("Gagal");
    output.into_raw()
}
