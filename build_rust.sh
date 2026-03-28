#!/bin/bash

# 1. Tentukan lokasi
JNI_DIR="app/src/main/jni"
LIBS_DIR="app/src/main/jniLibs/arm64-v8a"

echo "🚀 Memulai Build Rust untuk Odfiz Engine..."

# 2. Masuk ke folder Rust dan Build
cd $JNI_DIR
cargo build --target aarch64-linux-android --release

if [ $? -eq 0 ]; then
    echo "✅ Build Rust Berhasil!"
    
    # 3. Buat folder tujuan jika belum ada
    mkdir -p ../jniLibs/arm64-v8a
    
    # 4. Copy file .so ke folder Android
    cp target/aarch64-linux-android/release/libodfiz_native.so ../jniLibs/arm64-v8a/
    
    echo "📦 File .so sudah dipindahkan ke jniLibs."
    echo "💡 Sekarang Mas tinggal 'git push' atau build APK-nya."
else
    echo "❌ Build Gagal. Cek koneksi atau kode Rust Mas."
fi

cd ../../../
