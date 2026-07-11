from typing import TypedDict, List
from YouTube_Installer import Download_Video
# 定义全局状态的数据结构
class AgentState(TypedDict):
    messages: List[str]      # 对话历史
    downlaod_list: List[str]  # 视频下载链接
    # ... 任何其他需要追踪的状态


# 定义一个下载视频节点函数
def VideoDownloader(state: AgentState) -> AgentState:
    """根据下载列表下载对应YouTube视频"""
    download_list = state["downlaod_list"]
    try:
        Download_Video(download_list)
    except Exception as e:
        print(f"视频下载流程出错: {str(e)}")
    return state

