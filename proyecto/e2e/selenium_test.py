"""
Suite Selenium E2E para LlamaChat frontend.
Ejecutar con: python selenium_test.py
Requiere: Chrome instalado y chromedriver en PATH (o webdriver-manager lo gestiona).
"""
import os
import time
import unittest

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from webdriver_manager.chrome import ChromeDriverManager

FRONTEND_URL = os.environ.get("FRONTEND_URL", "http://localhost:5010")
TEST_EMAIL   = os.environ.get("TEST_EMAIL", "dsevilla@um.es")
TEST_PASS    = os.environ.get("TEST_PASS", "admin")
WAIT_TIMEOUT = int(os.environ.get("WAIT_TIMEOUT", "120"))


class LlamaChatE2E(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        options = Options()
        options.add_argument("--headless")
        options.add_argument("--no-sandbox")
        options.add_argument("--disable-dev-shm-usage")
        options.add_argument("--disable-gpu")
        service = Service(ChromeDriverManager().install())
        cls.driver = webdriver.Chrome(service=service, options=options)
        cls.wait = WebDriverWait(cls.driver, WAIT_TIMEOUT)

    @classmethod
    def tearDownClass(cls):
        cls.driver.quit()

    def test_01_login(self):
        """Accede al frontend, hace login y verifica redirección al índice."""
        self.driver.get(f"{FRONTEND_URL}/login")
        self.wait.until(EC.presence_of_element_located((By.NAME, "email")))

        self.driver.find_element(By.NAME, "email").send_keys(TEST_EMAIL)
        self.driver.find_element(By.NAME, "password").send_keys(TEST_PASS)
        self.driver.find_element(By.CSS_SELECTOR, "button[type='submit']").click()

        self.wait.until(EC.url_contains(FRONTEND_URL))
        self.assertNotIn("/login", self.driver.current_url)

    def test_02_navigate_to_chat(self):
        """Navega a /chat y verifica que se muestra el formulario."""
        self.driver.get(f"{FRONTEND_URL}/chat")
        self.wait.until(EC.presence_of_element_located((By.ID, "prompt")))
        self.assertIn("chat", self.driver.current_url)

    def test_03_send_prompt_and_receive_response(self):
        """Envía un prompt y espera hasta que LlamaChat responda."""
        self.driver.get(f"{FRONTEND_URL}/chat")
        self.wait.until(EC.presence_of_element_located((By.ID, "prompt")))

        prompt_input = self.driver.find_element(By.ID, "prompt")
        prompt_input.send_keys("Di hola en una palabra")
        self.driver.find_element(By.ID, "send-btn").click()

        # Esperar a que aparezca la respuesta real (sin clase 'loading')
        response_locator = (By.XPATH,
            "//*[contains(@class,'bot') and not(contains(@class,'loading')) and contains(.,'LlamaChat')]")
        self.wait.until(EC.presence_of_element_located(response_locator))

        response_elements = self.driver.find_elements(*response_locator)
        self.assertTrue(len(response_elements) > 0, "No se recibió respuesta de LlamaChat")
        self.assertGreater(len(response_elements[-1].text), 10, "La respuesta está vacía o es muy corta")

    def test_04_navigate_to_logs(self):
        """Navega a /logs y verifica que se muestra al menos una conversación."""
        self.driver.get(f"{FRONTEND_URL}/logs")
        self.wait.until(EC.presence_of_element_located((By.TAG_NAME, "main")))
        body_text = self.driver.find_element(By.TAG_NAME, "body").text
        self.assertTrue(
            "conversaciones" in body_text.lower() or "chat-" in body_text.lower(),
            "La página de logs no muestra conversaciones"
        )


if __name__ == "__main__":
    unittest.main(verbosity=2)
