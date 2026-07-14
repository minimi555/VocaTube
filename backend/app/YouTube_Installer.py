from typing import List
import yt_dlp
import os
from video_url.url_2 import url_2
from video_url.url_2 import sub_path

OUTPUT_VIDEO_DIR = f"/opt/assets/video/{sub_path}"
OUTPUT_zh_DIR = f"/opt/assets/subs/zh/{sub_path}"
OUTPUT_en_DIR = f"/opt/assets/subs/en/{sub_path}"


def _download_subtitles(url: str, output_dir: str, languages: List[str]) -> bool:
    """按优先级逐个尝试下载字幕，下载到第一个即停止。
    languages 列表顺序即优先级，如 ['zh', 'zh-Hans', 'zh-Hant', 'zh-TW']。
    返回 True 表示成功下载了字幕文件，False 表示所有语言都未找到。
    """
    os.makedirs(output_dir, exist_ok=True)
    outtmpl = os.path.join(output_dir, '%(title)s.%(ext)s')

    # 先获取视频信息，查看可用字幕列表
    with yt_dlp.YoutubeDL({'skip_download': True, 'quiet': True}) as ydl:
        info = ydl.extract_info(url, download=False)

    title = info.get('title', '')
    available_subs = info.get('subtitles', {})
    available_auto = info.get('automatic_captions', {})

    # 按优先级找第一个可用的语言
    target_lang = None
    for lang in languages:
        if lang in available_subs or lang in available_auto:
            target_lang = lang
            break

    if not target_lang:
        return False

    subtitle_opts = {
        'skip_download': True,
        'writesubtitles': True,
        'writeautomaticsub': True,
        'subtitleslangs': [target_lang],
        'subtitlesformat': 'srt',
        'outtmpl': outtmpl,
        'quiet': False,
        'no_warnings': False,
    }

    with yt_dlp.YoutubeDL(subtitle_opts) as ydl:
        ydl.extract_info(url, download=True)

    # 验证文件是否真正写入
    for f in os.listdir(output_dir):
        if f.endswith('.srt') and title in f:
            return True
    return False


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
                if _download_subtitles(url, OUTPUT_zh_DIR, ['zh', 'zh-Hans', 'zh-Hant', 'zh-TW']):
                    print(f"✓ 中文字幕下载成功: {url}")
                else:
                    print(f"⚠ 未找到中文字幕: {url}")
            except Exception as e:
                print(f"⚠ 中文字幕下载失败: {url}")
                print(f"  错误: {str(e)}")

            try:
                if _download_subtitles(url, OUTPUT_en_DIR, ['en', 'en-US']):
                    print(f"✓ 英文字幕下载成功: {url}")
                else:
                    print(f"⚠ 未找到英文字幕: {url}")
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
    test_urls = url_2  # 示例视频 URL
    try:
        Download_Video(test_urls)
    except Exception as e:
        print(f"下载过程中出现错误: {str(e)}")