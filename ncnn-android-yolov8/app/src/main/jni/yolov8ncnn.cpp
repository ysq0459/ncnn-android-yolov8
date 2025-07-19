// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>
#include <sys/stat.h>

#include <platform.h>
#include <benchmark.h>

#include "yolo.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgcodecs/imgcodecs.hpp>


#include <android/bitmap.h>
#include <android/log.h>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static int draw_unsupported(cv::Mat& rgb)
{
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat& rgb)
{
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        for (int i = 0; i < 10; i++)
        {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0;
    int x = rgb.cols - label_size.width;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

static Yolo* g_yolo = 0;
static ncnn::Mutex lock;
static cv::Mat g_latest_frame;
static std::vector<cv::Mat> g_latest_seg_frame_list;
static ncnn::Mutex g_frame_lock;

class MyNdkCamera : public NdkCameraWindow
{
public:
    virtual void on_image_render(cv::Mat& rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat& rgb) const
{
    // nanodet
    {
        ncnn::MutexLockGuard g(lock);

        if (g_yolo)
        {
            std::vector<Object> objects;
            g_yolo->detect(rgb, objects);
            std::vector<cv::Mat> cut_imgs;
            g_yolo->DetectResultCut(rgb, objects, cut_imgs);
            for (size_t i = 0; i < cut_imgs.size(); ++i) {
                std::vector<Object> seg_objs;
                g_yolo->segment(cut_imgs[i], seg_objs);
                // 绘制分割结果到源图像上
                g_yolo->draw_segment(cut_imgs[i], rgb, seg_objs, objects[i]);
            }

            g_yolo->draw(rgb, objects);
            g_latest_seg_frame_list = cut_imgs;
        }
        else
        {
            draw_unsupported(rgb);
        }
    }

    draw_fps(rgb);

    // 加锁更新最新帧
    {
        ncnn::MutexLockGuard lock(g_frame_lock);
        g_latest_frame = rgb.clone(); // 保存处理后的帧
    }
}

static MyNdkCamera* g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yolo;
        g_yolo = 0;
    }

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu)
{
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
    {
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char* modeltypes[] =
    {
        "testtube_ncnn"
    };

    const int target_sizes[] =
    {
        640
    };

    const int num_classes[] = {
        1
    };

    const float mean_vals[][3] =
    {
        {103.53f, 116.28f, 123.675f}
    };

    const float norm_vals[][3] =
    {
        { 1 / 255.f, 1 / 255.f, 1 / 255.f }
    };

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];
    int num_class = num_classes[(int)modelid];
    bool use_gpu = (int)cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0)
        {
            // no gpu
            delete g_yolo;
            g_yolo = 0;
        }
        else
        {
            if (!g_yolo)
                g_yolo = new Yolo;
            g_yolo->load(mgr, modeltype, target_size, num_class, mean_vals[(int)modelid], norm_vals[(int)modelid], use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_openCamera(JNIEnv* env, jobject thiz, jint facing)
{
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int)facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_closeCamera(JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_setOutputWindow(JNIEnv* env, jobject thiz, jobject surface)
{
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}

// 实现保存图像的 JNI 方法
JNIEXPORT jboolean JNICALL Java_com_tencent_yolov8ncnn_Yolov8Ncnn_saveCurrentImage(
        JNIEnv* env, jobject thiz, jstring jfolder, jstring jfilename) {

    // 获取 Java 传入的参数
    const char* folder = env->GetStringUTFChars(jfolder, nullptr);
    const char* filename = env->GetStringUTFChars(jfilename, nullptr);

    // 创建完整路径
    std::string base_path = "/sdcard/";
    std::string dir_path = base_path + folder;
    std::string file_path = dir_path + "/" + filename;

    // 创建目录（如果不存在）
    mkdir(dir_path.c_str(), 0777); // 创建文件夹

    // 加锁复制帧
    cv::Mat frame_copy;
    {
        ncnn::MutexLockGuard lock(g_frame_lock);
        if (g_latest_frame.empty()) {
            __android_log_print(ANDROID_LOG_ERROR, "ncnn", "No frame available to save");
            return JNI_FALSE;
        }
        frame_copy = g_latest_frame.clone();
        cv::cvtColor(frame_copy, frame_copy, cv::COLOR_RGB2BGR);
    }

    // 保存图像
    bool success = cv::imwrite(file_path, frame_copy);
    for (size_t i = 0; i < g_latest_seg_frame_list.size(); ++i) {
        cv::cvtColor(g_latest_seg_frame_list[i], g_latest_seg_frame_list[i], cv::COLOR_RGB2BGR);
        cv::imwrite(dir_path + "/seg_" + std::to_string(i) + ".jpg", g_latest_seg_frame_list[i]);
    }
    if (success) {
        __android_log_print(ANDROID_LOG_INFO, "ncnn", "Image saved: %s", file_path.c_str());
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "ncnn", "Failed to save image: %s", file_path.c_str());
    }

    // 释放资源
    env->ReleaseStringUTFChars(jfolder, folder);
    env->ReleaseStringUTFChars(jfilename, filename);

    return JNI_TRUE;
}

}
