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

#ifndef YOLO_H
#define YOLO_H

#include <opencv2/core/core.hpp>

#include <net.h>

struct Object
{
    cv::Rect_<float> rect;
    int label;
    float prob;
    cv::Mat mask;
    std::vector<float> mask_feat;
};
struct GridAndStride
{
    int grid0;
    int grid1;
    int stride;
};
class Yolo
{
public:
    Yolo();

    int load(const char* modeltype, int target_size, int _num_class, const float* mean_vals, const float* norm_vals, bool use_gpu = false);

    int load(AAssetManager* mgr, const char* modeltype, int target_size, int _num_class, const float* mean_vals, const float* norm_vals, bool use_gpu = false);

    int detect(const cv::Mat& rgb, std::vector<Object>& objects, float prob_threshold = 0.4f, float nms_threshold = 0.5f);

    int draw(const cv::Mat& rgb, const std::vector<Object>& objects);

    int segment(const cv::Mat& rgb, std::vector<Object>& objects, float prob_threshold = 0.4f, float nms_threshold = 0.5f);

    int draw_segment(cv::Mat& cut_img, cv::Mat& rgb, const std::vector<Object>& objects, const Object& img_obj);

    int DetectResultCut(const cv::Mat& rgb, const std::vector<Object>& objects, std::vector<cv::Mat>& cut_imgs);

    int SegmentSolution(cv::Mat& rgb, std::vector<Object>& objects, float prob_threshold = 0.4f, float nms_threshold = 0.5f);

    std::string get_model_name() {
        return model_name;
    }

private:
    ncnn::Net yolo;
    ncnn::Net yolo_solution;
    int target_size;
    int num_class;
    float mean_vals[3];
    float norm_vals[3];
    std::string model_name;
    ncnn::UnlockedPoolAllocator blob_pool_allocator;
    ncnn::PoolAllocator workspace_pool_allocator;
};

#endif // NANODET_H
