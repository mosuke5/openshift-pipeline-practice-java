# coding:utf-8
from time import sleep
from selenium import webdriver
from selenium.webdriver.chrome.options import Options

#Chromeを起動
chrome_options = Options()
chrome_options.add_argument('--headless')
chrome_options.add_argument('--no-sandbox')
chrome_options.add_argument('--disable-dev-shm-usage')
browser = webdriver.Chrome(options=chrome_options)

#表示したいサイトを開く
browser.get("https://blog.mosuke.tech/")

#表示したサイトのスクリーンショットを撮る
browser.save_screenshot('screen.png')

#表示した瞬間消えちゃうから幅を持たせておく
sleep(5)

#ブラウザを閉じる
browser.close()