import os
import re
from typing import Union

from sub.common_logger import setup_logging
from dateutil.parser import parse
from sub.driver_utils import Utilities
from selenium.common.exceptions import NoSuchElementException
from selenium.webdriver.common.by import By

logger = setup_logging(name=os.path.splitext(os.path.basename(__file__))[0])

def extract_digits(string) -> Union[int, None]:
    """Extracts first digits from the string.

    Args:
        string (str): string containing digits.

    Returns:
        int: digit that was extracted from the passed string
    """
    try:
        return int(re.search(r'\d+', string).group(0))
    except Exception as ex:
        logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at extract_digits : {}".format(ex))

class Finder:
    """
    this class should contain all the static method to find that accept
    webdriver instance and perform operation to find elements and return the
    found element.
    method should follow convention like so:

    @staticmethod
    def method_name(parameters):
    """

    @staticmethod
    def find_all_tweets(driver) -> list:
        """finds all tweets from the page"""
        try:
            return driver.find_elements(By.CSS_SELECTOR, '[data-testid="tweet"]')
        except Exception as ex:
            logger.exception(
                f"{os.getenv('EVENT_ID')} - "+"Error at method fetch_all_tweets : {}".format(ex))
            return []

    @staticmethod
    def find_replies(tweet) -> Union[int, str]:
        """finds replies from the tweet"""
        try:
            replies_element = tweet.find_element(
                By.CSS_SELECTOR, '[data-testid="reply"]')
            replies = replies_element.get_attribute("aria-label")
            return extract_digits(replies)
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at method find_replies : {}".format(ex))
            return ""

    @staticmethod
    def find_shares(tweet) -> Union[int, str]:
        """finds shares from the tweet"""
        try:
            shares_element = tweet.find_element(
                By.CSS_SELECTOR, '[data-testid="retweet"]')
            shares = shares_element.get_attribute("aria-label")
            return extract_digits(shares)
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at method find_shares : {}".format(ex))
            return ""

    @staticmethod
    def find_status(tweet) -> Union[list, tuple]:
        """finds status and link from the tweet"""
        try:
            anchor = tweet.find_element(
                By.CSS_SELECTOR, "a[aria-label][dir]")
            return (anchor.get_attribute("href").split("/"), anchor.get_attribute("href"))
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at method find_status : {}".format(ex))
            return []

    @staticmethod
    def find_all_anchor_tags(tweet) -> Union[list, None]:
        """finds all anchor tags from the tweet"""
        try:
            return tweet.find_elements(By.TAG_NAME, 'a')
        except Exception as ex:
            logger.exception(
                f"{os.getenv('EVENT_ID')} - "+"Error at method find_all_anchor_tags : {}".format(ex))

    @staticmethod
    def find_timestamp(tweet) -> Union[str, None]:
        """finds timestamp from the tweet"""
        try:
            timestamp = tweet.find_element(By.TAG_NAME,
                                           "time").get_attribute("datetime")
            posted_time = parse(timestamp).isoformat()
            return posted_time
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at method find_timestamp : {}".format(ex))

    @staticmethod
    def find_content(tweet) -> Union[str, None]:
        try:
            #content_element = tweet.find_element('.//*[@dir="auto"]')[4]
            content_element = tweet.find_element(By.CSS_SELECTOR, 'div[lang]')
            return content_element.text
        except NoSuchElementException:
            return ""
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at method find_content : {}".format(ex))

    @staticmethod
    def find_like(tweet) -> Union[int, None]:
        """finds the like of the tweet"""
        try:
            like_element = tweet.find_element(
                By.CSS_SELECTOR, '[data-testid="like"]')
            likes = like_element.get_attribute("aria-label")
            return extract_digits(likes)
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at method find_like : {}".format(ex))

    @staticmethod
    def find_images(tweet) -> Union[list, None]:
        """finds all images of the tweet"""
        try:
            image_element = tweet.find_elements(By.CSS_SELECTOR,
                                                'div[data-testid="tweetPhoto"]')
            images = []
            for image_div in image_element:
                href = image_div.find_element(By.TAG_NAME,
                                              "img").get_attribute("src")
                images.append(href)
            return images
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at method find_images : {}".format(ex))
            return []

    @staticmethod
    def find_videos(tweet) -> list:
        """finds all videos present in the tweet"""
        try:
            image_element = tweet.find_elements(By.CSS_SELECTOR,
                                                'div[data-testid="videoPlayer"]')
            videos = []
            for video_div in image_element:
                href = video_div.find_element(
                    By.TAG_NAME, "video").get_attribute("src")
                videos.append(href)
            return videos
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at method find_videos : {}".format(ex))
            return []

    @staticmethod
    def is_retweet(tweet) -> bool:
        """return if the tweet is whether re-tweet"""
        try:
            tweet.find_element(By.CSS_SELECTOR, 'div.r-92ng3h.r-qvutc0')
            return True
        except NoSuchElementException:
            return False
        except Exception as ex:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at method is_retweet : {}".format(ex))
            return False

    @staticmethod
    def find_name_from_tweet(tweet, is_retweet=False) -> Union[str, None]:
        """finds the name from the post"""
        try:
            name = "NA"
            anchors = Finder.find_all_anchor_tags(tweet)
            if len(anchors) > 2:
                if is_retweet:
                    name = tweet.find_element(
                        By.CSS_SELECTOR, '[data-testid="User-Name"] > div a').text
                else:
                    name = anchors[1].text.split("\n")[0]
            return name
        except Exception as ex:
            logger.exception(
                f"{os.getenv('EVENT_ID')} - "+"Error at method find_name_from_post : {}".format(ex))

    @staticmethod
    def find_external_link(tweet) -> Union[str, None]:
        """finds external link from the tweet"""
        try:
            card = tweet.find_element(
                By.CSS_SELECTOR, '[data-testid="card.wrapper"]')
            href = card.find_element(By.TAG_NAME, 'a')
            return href.get_attribute("href")

        except NoSuchElementException:
            return ""
        except Exception as ex:
            logger.exception(
                f"{os.getenv('EVENT_ID')} - "+"Error at method find_external_link : {}".format(ex))

    @staticmethod
    def find_profile_image_link(tweet) -> Union[str, None]:
        """finds profile image links

        Args:
            tweet: Tweet Element

        Returns:
            Union[str, None]: returns string containing image link.
        """
        try:
            return tweet.find_element(By.CSS_SELECTOR, 'img[alt][draggable="true"]').get_attribute('src')
        except Exception as ex:
            logger.warning(f"{os.getenv('EVENT_ID')} - "+"Error at find_profile_image_link : {}".format(ex))

    @staticmethod
    def find_graphql_key(driver, URL):
      try:
        driver.get(URL)
        Utilities.wait_until_completion(driver)
        URL = None
        for request in driver.requests:
          if 'TopicLandingPage' in request.url:
            URL = request.url
            break
        if not URL:
          logger.exception(f"{os.getenv('EVENT_ID')} - "+'Failed to find key!')
        logger.debug(f"{os.getenv('EVENT_ID')} - "+'Key Found!')
        return URL.split('/')[6]
      except Exception as ex:
        logger.warning(f"{os.getenv('EVENT_ID')} - "+'Error at find_graphql_link : {}'.format(ex))