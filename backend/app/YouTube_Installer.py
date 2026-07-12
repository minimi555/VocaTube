from typing import List
import yt_dlp
import os

OUTPUT_VIDEO_DIR = "/root/assets/video"
OUTPUT_zh_DIR = "/root/assets/subs/zh"
OUTPUT_en_DIR = "/root/assets/subs/en"


def _download_subtitles(url: str, output_dir: str, languages: List[str]) -> None:
    """为单个 URL 下载指定语言的字幕。"""
    os.makedirs(output_dir, exist_ok=True)
    subtitle_opts = {
        'skip_download': True,
        'writesubtitles': True,
        'subtitleslangs': languages,
        'subtitlesformat': 'srt',
        'outtmpl': os.path.join(output_dir, '%(title)s.%(ext)s'),
        'quiet': False,
        'no_warnings': False,
    }

    with yt_dlp.YoutubeDL(subtitle_opts) as ydl:
        ydl.extract_info(url, download=True)


def Download_Video(download_list: List):
    """
    下载 YouTube 视频列表，合并视频和音频为 mp4 文件，并额外下载中英文字幕。
    Args:
        download_list: YouTube URL 列表
    Raises:
        Exception: 当所有 URL 都下载失败时抛出异常
    """
    failed_urls = []

    # 确保输出目录存在
    os.makedirs(OUTPUT_VIDEO_DIR, exist_ok=True)
    os.makedirs(OUTPUT_zh_DIR, exist_ok=True)
    os.makedirs(OUTPUT_en_DIR, exist_ok=True)

    # yt-dlp 配置：分别下载最佳视频流和音频流，用 ffmpeg 合并为 mp4
    # 注意：bestvideo+bestaudio 需要系统安装 ffmpeg；若不可用则回退到 best 预合并流
    ydl_opts = {
        'format': 'bestvideo*+bestaudio/best',
        'merge_output_format': 'mp4',
        'outtmpl': os.path.join(OUTPUT_VIDEO_DIR, '%(title)s.%(ext)s'),
        'quiet': False,
        'no_warnings': False,
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        for url in download_list:
            try:
                print(f"正在下载视频: {url}")
                ydl.extract_info(url, download=True)
                print(f"✓ 视频下载成功: {url}")
            except Exception as e:
                print(f"✗ 视频下载失败: {url}")
                print(f"  错误: {str(e)}")
                failed_urls.append(url)
                continue

            try:
                _download_subtitles(url, OUTPUT_zh_DIR, ['zh'])
                print(f"✓ 中文字幕下载成功: {url}")
            except Exception as e:
                print(f"⚠ 中文字幕下载失败: {url}")
                print(f"  错误: {str(e)}")

            try:
                _download_subtitles(url, OUTPUT_en_DIR, ['en-US'])
                print(f"✓ 英文字幕下载成功: {url}")
            except Exception as e:
                print(f"⚠ 英文字幕下载失败: {url}")
                print(f"  错误: {str(e)}")

    # 下载结束后报告失败的 URL
    if failed_urls:
        error_msg = f"下载完成，但有 {len(failed_urls)} 个 URL 失败:\n"
        for url in failed_urls:
            error_msg += f"  - {url}\n"
        raise Exception(error_msg)


if __name__ == '__main__':
    # 测试下载功能
    test_urls = [
        "https://www.youtube.com/watch?v=aAy-B6KPld8",  # 示例视频 URL
    ]
    try:
        Download_Video(test_urls)
    except Exception as e:
        print(f"下载过程中出现错误: {str(e)}")