from fake_headers import Headers
# to add capabilities for chrome and firefox, import their Options with different aliases
from selenium.webdriver.chrome.options import Options as CustomChromeOptions
from selenium.webdriver.firefox.options import Options as CustomFireFoxOptions
from selenium import webdriver


class Initializer:
    def __init__(self, browser_name: str, headless: bool):
        """Initialize Browser

        Args:
            browser_name (str): Browser Name
            headless (bool): Whether to run Browser in headless mode?
            proxy (Union[str, None], optional): Optional parameter, if user wants to use proxy for scraping. If the proxy is authenticated proxy then the proxy format is username:password@host:port. Defaults to None.
      """
        self.browser_name = browser_name
        self.headless = headless

    def set_properties(self, browser_option):
        """adds capabilities to the driver"""
        header = Headers(os="lin",browser='chrome',headers=True).generate()['User-Agent']
        if self.headless:
            # runs browser in headless mode
            browser_option.add_argument("--headless")
        browser_option.add_argument('--no-sandbox')
        browser_option.add_argument("--disable-dev-shm-usage")
        browser_option.add_argument('--ignore-certificate-errors')
        browser_option.add_argument('--disable-gpu')
        browser_option.add_argument('--log-level=3')
        browser_option.add_argument('--disable-notifications')
        browser_option.add_argument('--disable-popup-blocking')
        browser_option.add_argument('--user-agent={}'.format(header))
        return browser_option

    def set_driver_for_browser(self, browser_name: str):
        """expects browser name and returns a driver instance"""
        # if browser is suppose to be chrome
        if browser_name.lower() == "chrome":
            browser_option = CustomChromeOptions()
            return webdriver.Chrome(options=self.set_properties(browser_option))
        elif browser_name.lower() == "firefox":
            browser_option = CustomFireFoxOptions()
            return webdriver.Firefox(options=self.set_properties(browser_option))
        else:
            # if browser_name is not chrome neither firefox than raise an exception
            raise Exception("Browser not supported!")

    def init(self):
        """returns driver instance"""
        driver = self.set_driver_for_browser(self.browser_name)
        return driver