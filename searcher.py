from langchain.agents import create_agent
from langchain_deepseek import ChatDeepSeek
from langchain_tavily import TavilySearch
from langchain_core.tools import tool
from typing import TypedDict, List
from datetime import date
#import signal

from dotenv import load_dotenv
load_dotenv()


@tool
def get_current_date() -> str:
    """返回当前日期（只包含年月日），格式为“YYYY年M月D日”。"""
    today = date.today()
    return f"{today.year}年{today.month}月{today.day}日"

from langchain_tavily import TavilySearch
QS_200_list : List[str] = ["cuhk.edu.hk"]
tavily_search = TavilySearch(
    max_results=5,
    topic="general",
    # include_answer=False,
    # include_raw_content=False,
    # include_images=False,
    # include_image_descriptions=False,
    search_depth="basic",
    # time_range="day",
    # start_date=None,
    # end_date=None,
    include_domains=QS_200_list #"www.chinaielts.org", "toefl.neea.cn", "www.ets.org"
    # exclude_domains=None,
    # include_usage= False
)
# Initialize the agent with the search tool
agent = create_agent(
    model=ChatDeepSeek(model="deepseek-v4-flash", timeout=None),
    tools=[tavily_search, get_current_date],
    system_prompt="你是一个针对QS前200的学校的web检索助手，你可以根据用户的提问，检索相关学校的官方网站内容，并提供准确的回答。请确保你的回答基于可靠的来源，并尽量引用官方网站的信息并附上具体网址。注意信息来源时间和用户提问时间，确保信息的时效性。若用户指明自己的意向专业，请搜索各个学校的官方网站，提供该该学校专业对英语要求的详细信息，并附上具体网址。"
)
print("Agent initialized and ready to use.")
# Use the agent
print("searching...")
current_date = get_current_date.invoke({})
response = agent.invoke(
    {"messages": [{"role": "user", "content": f"当前时间为{current_date}," + "香港中文大学计算机相关硕士项目的对英语雅思和托福的要求"}]}
    #,config={"recursion_limit": 12}
)

# 打印最终回答
print("\n===== 最终回答 =====")
print(response["messages"][-1].content)