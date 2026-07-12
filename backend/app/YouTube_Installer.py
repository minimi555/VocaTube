from typing import List
import yt_dlp
import os

OUTPUT_DIR = "/root/assets/video"

def Download_Video(download_list: List):
    """
    下载 YouTube 视频列表，合并视频和音频为 mp4 文件。
    Args:
        download_list: YouTube URL 列表
    Raises:
        Exception: 当所有 URL 都下载失败时抛出异常
    """
    failed_urls = []

    # 确保输出目录存在
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # yt-dlp 配置：分别下载最佳视频流和音频流，用 ffmpeg 合并为 mp4
    # 注意：bestvideo+bestaudio 需要系统安装 ffmpeg；若不可用则回退到 best 预合并流
    ydl_opts = {
        'format': 'bestvideo*+bestaudio/best',
        'merge_output_format': 'mp4',
        'outtmpl': os.path.join(OUTPUT_DIR, '%(title)s.%(ext)s'),
        'quiet': False,
        'no_warnings': False,
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        for url in download_list:
            try:
                print(f"正在下载: {url}")
                ydl.extract_info(url, download=True)
                print(f"✓ 下载成功: {url}")
            except Exception as e:
                print(f"✗ 下载失败: {url}")
                print(f"  错误: {str(e)}")
                failed_urls.append(url)

    # 下载结束后报告失败的 URL
    if failed_urls:
        error_msg = f"下载完成，但有 {len(failed_urls)} 个 URL 失败:\n"
        for url in failed_urls:
            error_msg += f"  - {url}\n"
        raise Exception(error_msg)
    
if __name__ == '__main__':
    # 测试下载功能
    test_urls = [
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",  # 示例视频 URL
    ]
    try:
        Download_Video(test_urls)
    except Exception as e:
        print(f"下载过程中出现错误: {str(e)}")