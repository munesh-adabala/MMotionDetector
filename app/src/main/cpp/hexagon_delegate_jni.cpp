//
// Created by Administrator on 03-04-2020.
//

#include "hexagon_delegate_jni.h"


#include <jni.h>

#include <sstream>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_telxsi_dms_tflite_HexagonDelegate_createDelegate(
    JNIEnv* env, jclass clazz) {
  // Auto-choosing the best performing config for closed release.
  TfLiteHexagonDelegateOptions options = {0};
  TfLiteHexagonInit();
  return reinterpret_cast<jlong>(TfLiteHexagonDelegateCreate(&options));
}

JNIEXPORT void JNICALL
Java_com_telxsi_dms_tflite_HexagonDelegate_HexagonDelegate_deleteDelegate(
    JNIEnv* env, jclass clazz, jlong delegate) {
  TfLiteHexagonDelegateDelete(reinterpret_cast<TfLiteDelegate*>(delegate));
  TfLiteHexagonTearDown();
}

JNIEXPORT jboolean JNICALL
Java_com_telxsi_dms_tflite_HexagonDelegate_HexagonDelegate_setAdspLibraryPath(
    JNIEnv* env, jclass clazz, jstring native_lib_path) {
  const char* lib_dir_path = env->GetStringUTFChars(native_lib_path, nullptr);
  std::stringstream path;
  path << lib_dir_path
       << ";/system/lib/rfsa/adsp;/system/vendor/lib/rfsa/adsp;/dsp";
  return setenv("ADSP_LIBRARY_PATH", path.str().c_str(), 1 /*override*/) == 0
             ? JNI_TRUE
             : JNI_FALSE;
}

#ifdef __cplusplus
}  // extern "C"
#endif  // __cplusplus
