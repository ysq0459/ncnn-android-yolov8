cmake_minimum_required(VERSION 3.10)

project(ncnnDemo)

# find_package(PkgConfig)
find_package(OpenCV REQUIRED)

# # 添加OpenMP支持
FIND_PACKAGE(OpenMP REQUIRED)  
if(OPENMP_FOUND)  
    message("OPENMP FOUND")  
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${OpenMP_C_FLAGS}")  
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")  
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${OpenMP_EXE_LINKER_FLAGS}")  
endif() 


# 分别设置ncnn的链接库和头文件
set(NCNN_LIBS /home/yaosiqi/thirdpkg/ncnn/build/install/lib/libncnn.a)
set(NCNN_INCLUDE_DIRS /home/yaosiqi/thirdpkg/ncnn/build/install/include/ncnn)
set(CMAKE_BUILD_TYPE Debug)

# 头文件路径
message(STATUS "OPENCV_INCLUDE_DIRS: ${OpenCV_INCLUDE_DIRS}")
include_directories(${OpenCV_INCLUDE_DIRS})
include_directories(${NCNN_INCLUDE_DIRS})

link_directories(/home/yaosiqi/thirdpkg/ncnn/build/install/lib)

add_executable(a yolov8-seg.cpp) # 要运行的cpp文件
target_link_libraries(a ${OpenCV_LIBS} ) #  
target_link_libraries(a ${NCNN_LIBS} ) 
target_link_libraries(a -lpthread)