# coding:utf-8
import sys
from time import sleep
from selenium import webdriver
from selenium.webdriver.chrome.options import Options

args = sys.argv

chrome_options = Options()
chrome_options.add_argument('--headless')
chrome_options.add_argument('--no-sandbox')
chrome_options.add_argument('--disable-dev-shm-usage')
browser = webdriver.Chrome(options=chrome_options)

browser.get(args[1])
assert browser.title == ''

# Write your test codes
