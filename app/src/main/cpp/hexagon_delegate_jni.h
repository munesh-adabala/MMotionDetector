//
// Created by Administrator on 03-04-2020.
//

#ifndef DMS_SNAPDRAGON_DENS_CAM_HEXAGON_DELEGATE_JNI_H
#define DMS_SNAPDRAGON_DENS_CAM_HEXAGON_DELEGATE_JNI_H

#include "common.h"

#ifdef SWIG
#define TFL_CAPI_EXPORT
#else
#if defined(_WIN32)
#ifdef TFL_COMPILE_LIBRARY
#define TFL_CAPI_EXPORT __declspec(dllexport)
#else
#define TFL_CAPI_EXPORT __declspec(dllimport)
#endif  // TFL_COMPILE_LIBRARY
#else
#define TFL_CAPI_EXPORT __attribute__((visibility("default")))
#endif  // _WIN32
#endif  // SWIG

#ifdef __cplusplus
extern "C" {
#endif  // __cplusplus
struct TFL_CAPI_EXPORT TfLiteHexagonDelegateOptions {
  // This corresponds to the debug level in the hexagon SDK. 0 (default)
  // means no debug.
  int debug_level;
  // This corresponds to powersave_level in the hexagon SDK.
  // where 0 (default) means high performance which means more power
  // consumption.
  int powersave_level;
  // If set to true, performance information about the graph will be dumped
  // to Standard output, this includes cpu cycles.
  // WARNING: Experimental and subject to change anytime.
  bool print_graph_profile;
  // If set to true, graph structure will be dumped to Standard output.
  // This is usually beneficial to see what actual nodes executed on
  // the DSP. Combining with 'debug_level' more information will be printed.
  // WARNING: Experimental and subject to change anytime.
  bool print_graph_debug;
};

// Return a delegate that uses Hexagon SDK for ops execution.
// Must outlive the interpreter.
TfLiteDelegate* TFL_CAPI_EXPORT
TfLiteHexagonDelegateCreate(const TfLiteHexagonDelegateOptions* options);

// Do any needed cleanup and delete 'delegate'.
void TFL_CAPI_EXPORT TfLiteHexagonDelegateDelete(TfLiteDelegate* delegate);

// Initializes the DSP connection.
// This should be called before doing any usage of the delegate.
// "lib_directory_path": Path to the directory which holds the
// shared libraries for the Hexagon NN libraries on the device.
void TFL_CAPI_EXPORT TfLiteHexagonInitWithPath(const char* lib_directory_path);

// Same as above method but doesn't accept the path params.
// Assumes the environment setup is already done. Only initialize Hexagon.
void TFL_CAPI_EXPORT TfLiteHexagonInit();

// Clean up and switch off the DSP connection.
// This should be called after all processing is done and delegate is deleted.
void TFL_CAPI_EXPORT TfLiteHexagonTearDown();
#ifdef __cplusplus
}
#endif  // __cplusplus

#endif  // TENSORFLOW_LITE_EXPERIMENTAL_DELEGATES_HEXAGON_HEXAGON_DELEGATE_H_