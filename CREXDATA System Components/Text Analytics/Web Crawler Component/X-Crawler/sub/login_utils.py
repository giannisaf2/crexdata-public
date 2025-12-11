
import re
import os
import email
import random
import imaplib
import threading
from time import sleep

from email.utils import parsedate_to_datetime
from sub.common_logger import setup_logging
from sub.driver_utils import Utilities
from selenium.common.exceptions import NoSuchElementException
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys

logger = setup_logging(name=os.path.splitext(os.path.basename(__file__))[0])


def get_x_confirmation_code_from_gmail(gmail_address, gmail_app_password):
    """
    Connects to the gmail inbox and searches for the most recent email from `info@x.com`
    matching a confirmation code pattern in the subject. Returns the code or None.
    """
    IMAP_SERVER = 'imap.gmail.com'
    subject_pattern = re.compile (r"Your X confirmation code is (.+)")
    sender_email = 'info@x.com'

    try:
        # Connect to the server and login
        mail = imaplib.IMAP4_SSL(IMAP_SERVER)
        mail.login(gmail_address, gmail_app_password)
        mail.select("inbox")

        # Search for emails from the specified sender
        status, messages = mail.search(None, f'(UNSEEN FROM "{sender_email}")')
        email_ids = messages[0].split()
        matches = []

        for eid in email_ids:
            status, data = mail.fetch(eid, '(RFC822)')
            raw_email = data[0][1]
            msg = email.message_from_bytes(raw_email)

            subject = msg["Subject"]
            date_str = msg["Date"]

            match = subject_pattern.search(subject or "")
            if match:
                try:
                    date_obj = parsedate_to_datetime(date_str)
                    matches.append((date_obj, match.group(1)))
                except Exception:
                    continue  

        mail.logout()

        if matches:
            latest_match = sorted(matches, key=lambda x: x[0], reverse=True)[0]
            return latest_match[1]  

    except imaplib.IMAP4.error as e:
        print(f"IMAP error: {e}")
        return None
    except Exception as e:
        print(f"Unexpected error: {e}")
        return None
    return None

def check_exists_by_xpath(xpath, driver):
    try:
        driver.find_element(by=By.XPATH, value=xpath)
    except NoSuchElementException as ex:
        logger.warning(f"{os.getenv('EVENT_ID')} - "+'Xpath not found: {}'.format(ex))
        return False
    return True

def log_in(driver, wait=4): 

    driver.get('https://twitter.com/i/flow/login')

    email_xpath = '//input[@autocomplete="username"]'
    password_xpath = '//input[@autocomplete="current-password"]'
    username_xpath = '//input[@data-testid="ocfEnterTextTextInput"]'

    sleep(random.uniform(wait, wait + 1))

    # enter email
    if check_exists_by_xpath(email_xpath, driver):
        email_el = driver.find_element(by=By.XPATH, value=email_xpath)
        sleep(random.uniform(wait, wait + 1))
        email_el.send_keys(os.getenv('EMAIL'))
        sleep(random.uniform(wait, wait + 1))
        email_el.send_keys(Keys.RETURN)
        sleep(random.uniform(wait, wait + 1))
        logger.info(f"{os.getenv('EVENT_ID')} - "+"email entered successfully")
        print("email entered successfully")
    
    # in case twitter spotted unusual login activity : enter your username
    if check_exists_by_xpath(username_xpath, driver):
        username_el = driver.find_element(by=By.XPATH, value=username_xpath)
        sleep(random.uniform(wait, wait + 1))
        username_el.send_keys(os.getenv('X_USERNAME'))
        sleep(random.uniform(wait, wait + 1))
        username_el.send_keys(Keys.RETURN)
        sleep(random.uniform(wait, wait + 1))
        logger.info(f"{os.getenv('EVENT_ID')} - "+"username entered successfully")
        print("username entered successfully")
    
    # enter password
    if check_exists_by_xpath(password_xpath, driver):
        password_el = driver.find_element(by=By.XPATH, value=password_xpath)
        password_el.send_keys(os.getenv('X_PASSWORD'))
        sleep(random.uniform(wait, wait + 1))
        password_el.send_keys(Keys.RETURN)
        sleep(random.uniform(wait, wait + 1))
        logger.info(f"{os.getenv('EVENT_ID')} - "+"password entered successfully")
        print("password entered successfully")
    
    # in case twitter spotted unusual login activity : get confirmation code for configured email
    if check_exists_by_xpath(username_xpath, driver):
        confirmation_el = driver.find_element(by=By.XPATH, value=username_xpath)
        logger.info(f"{os.getenv('EVENT_ID')} - "+"Getting X confirmation code from registered email")
        print("Getting X confirmation code from registered email")
        sleep(random.uniform(wait, wait + 1))
        for i in range(5):
            code = get_x_confirmation_code_from_gmail(os.getenv('EMAIL'), os.getenv('EMAIL_PASS'))
            if code is None:
                sleep(60)
            else:
                break  
        if code is None:
            logger.exception(f"{os.getenv('EVENT_ID')} - "+"Error at login: confirmation couldn't be gotten from email")
            raise Exception("Error at login: confirmation couldn't be gotten from email")
        confirmation_el.send_keys(code)
        sleep(random.uniform(wait, wait + 1))
        confirmation_el.send_keys(Keys.RETURN)
        sleep(random.uniform(wait, wait + 1))
        logger.info(f"{os.getenv('EVENT_ID')} - "+"confirmation code entered successfully")
        print("confirmation code entered successfully")        
    
  
    Utilities.wait_until_completion(driver)