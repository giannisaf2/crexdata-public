import re
import os
import sys
import json
import signal
import argparse
import dateutil.parser

from selenium.webdriver.common.by import By
from selenium.common.exceptions import NoSuchElementException

current = os.path.dirname(os.path.realpath(__file__))
parent = os.path.dirname(current)
sys.path.append(parent)
from sub.login_utils import log_in
from sub.element_finder import Finder
from sub.driver_utils import Utilities
from sub.common_logger import setup_logging
from sub.driver_initialization import Initializer
from confluent_kafka import Producer
from config import KAFKA_CONFIG, KAFKA_TWEETS_TOPIC 

logger = setup_logging(name=os.path.splitext(os.path.basename(__file__))[0])
driver = None


# Init producer               
TOPIC = KAFKA_TWEETS_TOPIC                                         
PRODUCER = Producer(**KAFKA_CONFIG)


# Handle SIGTERM (graceful shutdown)
def graceful_shutdown(signum, frame):
    logger.info(f"{os.getenv('EVENT_ID')} - "+"Received SIGTERM in crawler_kafka.py. Cleaning up...")
    print("Received SIGTERM in crawler_kafka.py. Cleaning up...")
    try:
        PRODUCER.flush()
        driver.close()
        driver.quit() 
    except:
        pass
    finally:
        logger.info(f"{os.getenv('EVENT_ID')} - "+"Clean up done in crawler_kafka.py, Exiting....")
        print("Clean up done in crawler_kafka.py, Exiting....")
        sys.exit(0)

# Register the signal handler
signal.signal(signal.SIGTERM, graceful_shutdown)


def check_tweets_presence(tweet_list):
    if len(tweet_list) <= 0:
        print(f"No tweets")
        return False 
    else:
        return True


def check_if_tweet_is_Ad(tweet):
    try:
        dir_elms = tweet.find_elements(By.CSS_SELECTOR, 'div[dir="ltr"]')
        for elm in dir_elms:
            if elm.text == 'Ad':
                return True
    except NoSuchElementException as ex:
        return False
    return False

    
def url_generator(since=None, until_local=None, lang=None, display_type='Latest', words=None, to_account=None, from_account=None, mention_account=None,
                    hashtag=None, filter_replies=True, proximity=False, geocode=None, minreplies=None, minlikes=None, minretweets=None):
    """ 
    Generate twitter url with sepecific filters and advanced search options 
    """
    
    # format the <from_account>, <to_account> and <hash_tags>
    from_account = "(from%3A" + from_account + ")%20" if from_account is not None else ""
    to_account = "(to%3A" + to_account + ")%20" if to_account is not None else ""
    mention_account = "(%40" + mention_account + ")%20" if mention_account is not None else ""
    
    hash_tags = "(%23" + hashtag + ")%20" if hashtag is not None else ""

    if words is not None:
        word_list = [x.strip() for x in words.split(',')]
        if len(word_list) == 1:
            words = "(" + str(''.join(word_list)) + ")%20"
        else:
            words = "(" + str('%20OR%20'.join(word_list)) + ")%20"
    else:
        words = ""

    if lang is not None:
        lang = 'lang%3A' + lang
    else:
        lang = ""

    # until_local & since should have date as str in this format YYYY-MM-DD
    if until_local is not None: 
        until_local = "until%3A" + until_local + "%20" #
    else: 
        until_local = ""
    
    if since is not None: 
        since = "since%3A" + since + "%20"
    else: 
        since = ""

    if display_type == "Latest" or display_type == "latest":
        display_type = "&f=live"
    elif display_type == "Image" or display_type == "image":
        display_type = "&f=image"
    else:
        display_type = ""

    # filter replies
    if filter_replies == True:
        filter_replies = "%20-filter%3Areplies"
    else:
        filter_replies = ""
    # geo
    if geocode is not None:
        geocode = [x.strip() for x in geocode.split(',')] 
        geocode = "%20geocode%3A" + str('%2C'.join(geocode)) #geocode%3A20.87397%2C-156.677892%2C10km
    else:
        geocode = ""
    # min number of replies
    if minreplies is not None:
        minreplies = "%20min_replies%3A" + str(minreplies)
    else:
        minreplies = ""
    # min number of likes
    if minlikes is not None:
        minlikes = "%20min_faves%3A" + str(minlikes)
    else:
        minlikes = ""
    # min number of retweets
    if minretweets is not None:
        minretweets = "%20min_retweets%3A" + str(minretweets)
    else:
        minretweets = ""

    # proximity
    if proximity == True:
        proximity = "&lf=on"  # at the end
    else:
        proximity = ""

    url_query = 'https://twitter.com/search?q=' + words + from_account + to_account + mention_account + hash_tags + until_local + since + \
        lang + filter_replies + geocode + minreplies + minlikes + minretweets + '&src=typed_query' + display_type + proximity
    return url_query


def get_tweet_elms(tweet):
    tweet_dict = {}
    
    status, tweet_dict["tweet_url"] = Finder.find_status(tweet)
    tweet_dict["tweet_username"] = tweet_dict["tweet_url"].split("/")[3]
    tweet_dict["tweet_id"] = status[-1]
    tweet_dict["tweet_timestamp"] = Finder.find_timestamp(tweet)
    content = Finder.find_content(tweet)
    tweet_dict["tweet_text"] = re.sub('\n', '', content)
            
    dt = dateutil.parser.isoparse(tweet_dict["tweet_timestamp"])
    unix_time = int(dt.timestamp())
            
    return tweet_dict, unix_time


def delivery_callback(err, event):
    if err is not None:
        print(f'Delivery failed on reading for {event.value().decode("utf8")}: {err}')
    else:
        print(f'Temp reading for {event.value().decode("utf8")} produced to {event.topic()}')


def parse_args():
    parser = argparse.ArgumentParser(description="Script to scrape tweets")
    parser.add_argument("--username", type=str, required=True, help="Twitter username to download tweets.")
    parser.add_argument("--password", type=str, required=True, help="Twitter password to download tweets.")
    parser.add_argument("--email", type=str, required=True, help="Twitter email to download tweets.")
    parser.add_argument("--emailpass", type=str, required=True, help="App pass to access email inbox.")
    parser.add_argument("--geocode", type=str, required=True, help="String in for lat,lon,radium<km> example: 41.3825,2.176944,100km")
    parser.add_argument("--day", type=str, required=True, help="String of today's date in form 2025-03-24")
    parser.add_argument("--lang", type=str, help="Language to crawl from X in ISO format ex: en,de, defaults to all langs", default=None)
    parser.add_argument("--identifier", type=str, required=True, help="Crawl identifier")
    args = parser.parse_args()
    
    return args
    
    
def scrape_latest(keywords, geocode_radius, start_date, end_date, tweet_lang, filter_replies, identity):
    """
    keywords (string: keywords to look for separated by comma, ex: 'hawaii,wildfire,maui,fire') - str
    geocode_radius (string: geocode with radius in form lat,lon,radius, ex: '20.87397, -156.677892, 1000km') - str
    start_date (string: date of date range in form YYYY-MM-DD, ex: '2023-08-06') - str
    end_date (string: end date of date range in form YYYY-MM-DD, ex: '2023-08-06') - str
    tweet_lang (string: language to get tweets, ex: en) - str
    filter_replies (boolean: filter out tweets that are replies? ex: True or False) - bool
    uname (string: your x username for login purpose) - str 
    psswd (string: your x password for login purpose) - str
    email (string: your email for login purpose) - str
    """
    
    try:      
        # Generate url
        url = url_generator(since=start_date, until_local=end_date, lang=tweet_lang, words=keywords,\
                filter_replies=filter_replies, geocode=geocode_radius)
        
        logger.info(f"{os.getenv('EVENT_ID')} - "+f'Getting tweets from URL:\n{url}')
        
        # Init driver
        global driver
        driver = Initializer(browser_name='chrome', headless=True).init()
        logger.info(f"{os.getenv('EVENT_ID')} - "+'Driver initialized')
        
        # gets the url page
        driver.get(url)
        
        # Wait until page loads then get tweet elements
        Utilities.wait_until_completion(driver)
        Utilities.wait_until_tweets_appear(driver)

        current_url = driver.current_url
        
        if 'login' in current_url:
            logger.info(f"{os.getenv('EVENT_ID')} - "+f'Logging in')
            log_in(driver=driver, wait=5)
            driver.get(url)
            Utilities.wait_until_completion(driver)
            Utilities.wait_until_tweets_appear(driver)
        
        # Tweet processing
        logger.info(f"{os.getenv('EVENT_ID')} - "+'Fetching and storing tweets')
        tweetId_buffer = []
        present_tweets = Finder.find_all_tweets(driver)
        
        while True: 
            # to ensure large list is cleared
            if len(tweetId_buffer) > 200:
                tweetId_buffer = []
            for tweet in present_tweets:
                if check_if_tweet_is_Ad(tweet): # Check if tweet is an Ad and discard
                    continue
                
                tweet_dict, _ = get_tweet_elms(tweet)
                
                if tweet_dict["tweet_text"] == '' or tweet_dict["tweet_text"].isspace():
                    continue
                
                if tweet_dict["tweet_id"] not in tweetId_buffer:
                    tweetId_buffer.append(tweet_dict["tweet_id"])
                else:
                    continue
                
                tweet_dict["event_identifier"] = identity
                #print(tweet_dict)
                
                try:
                    PRODUCER.produce(TOPIC, json.dumps(tweet_dict), callback=delivery_callback) #json.dumps(msg)
                except BufferError:
                    print(f'Local producer queue is full ({len(PRODUCER)} messages awaiting delivery): try again\n')
                PRODUCER.poll(0)
            PRODUCER.flush()    
                
            Utilities.scroll_latest_refresh(driver)
            Utilities.wait_until_completion(driver)
            Utilities.wait_until_tweets_appear(driver)
            present_tweets = Finder.find_all_tweets(driver)
        
    except Exception as ex:
        PRODUCER.flush()
        logger.exception(
            f"{os.getenv('EVENT_ID')} - "+"Error at method scrape_tweets on : {}".format(ex))
        driver.close()
        driver.quit() 
        
    except KeyboardInterrupt:
        PRODUCER.flush()
        logger.info(f"{os.getenv('EVENT_ID')} - "+"Tweet collection aborted by user")
        driver.close()
        driver.quit() 
                

if __name__ == "__main__": 
    args = parse_args()
    
    # Set envs
    os.environ['EMAIL'] = args.email
    os.environ['EMAIL_PASS'] = args.emailpass
    os.environ['X_USERNAME'] = args.username
    os.environ['X_PASSWORD'] = args.password
    os.environ['EVENT_ID'] = args.identifier

    
    scrape_latest(keywords=None, 
                  geocode_radius=args.geocode, 
                  start_date=args.day, 
                  end_date=None, 
                  tweet_lang=args.lang, 
                  filter_replies=False, 
                  identity=args.identifier
                  )
    
    
