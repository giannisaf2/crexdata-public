import os
import time
from random import randint

from sub.common_logger import setup_logging
from selenium.common.exceptions import (NoSuchElementException,
                                        WebDriverException)
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

logger = setup_logging(name=os.path.splitext(os.path.basename(__file__))[0])


class Utilities:
    """
    this class contains all the method related to driver behaviour,
    like scrolling, waiting for element to appear, it contains all static
    method, which accepts driver instance as a argument

    @staticmethod
    def method_name(parameters):
    """

    @staticmethod
    def wait_until_tweets_appear(driver) -> None:
        """Wait for tweet to appear. Helpful to work with the system facing
        slow internet connection issues
        """
        try:
            WebDriverWait(driver, 10).until(EC.presence_of_element_located(
                (By.CSS_SELECTOR, '[data-testid="tweet"]')))
        except WebDriverException:
            logger.exception(
                f"{os.getenv('EVENT_ID')} - "+"Tweets did not appear!, Try setting headless=False to see what is happening")

    @staticmethod
    def scroll_down(driver) -> None:
        """Helps to scroll down web page"""
        try:
            body = driver.find_element(By.CSS_SELECTOR, 'body')
            for _ in range(randint(1, 3)):
                body.send_keys(Keys.PAGE_DOWN)
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at scroll_down method {}".format(ex))

    @staticmethod
    def wait_until_completion(driver) -> None:
        """waits until the page have completed loading"""
        try:
            state = ""
            while state != "complete":
                time.sleep(randint(3, 5))
                state = driver.execute_script("return document.readyState")
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+'Error at wait_until_completion: {}'.format(ex))
            
    @staticmethod
    def scroll_latest_refresh(driver) -> None:
        """Helps to scroll up and down to refresh and collect latest"""
        try:
            body = driver.find_element(By.CSS_SELECTOR, 'body')
            body.send_keys(Keys.END)
            body.send_keys(Keys.HOME)
            time.sleep(randint(2, 3))
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at scroll_up_down method {}".format(ex))
            
