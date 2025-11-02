import { chromium, type FullConfig } from '@playwright/test';
import axios from 'axios';

async function globalSetup(config: FullConfig) {
  // You can perform global setup tasks here, such as starting a server or setting environment variables
  const path = await import("path");
  process.env.TESTS_DIR = path.resolve(process.cwd());
}

export default globalSetup;