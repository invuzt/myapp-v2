use jni::JNIEnv;
use jni::objects::{JClass, JByteArray};
use jni::sys::jboolean;

#[no_mangle]
pub extern "system" fn Java_com_odfiz_timemark_EditorActivity_processPhotoNative(
    mut _env: JNIEnv,
    _class: JClass,
    _photo_data: JByteArray,
) -> jboolean {
    // Di sini Rust akan mengolah byte foto secara langsung di memori
    // Tanpa membuat objek Bitmap Java yang mengganduli RAM
    1 // Return True (Sukses)
}

#[no_mangle]
pub extern "system" fn Java_com_odfiz_timemark_EditorActivity_helloFromRust(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = env.new_string("Odfiz Engine: Rust Aktif & Bypass JNI Siap!").expect("Gagal");
    output.into_raw()
}

use jni::sys::jstring;
