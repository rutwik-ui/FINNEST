import {defineConfig, devices} from '@playwright/test';
export default defineConfig({
 testDir:'./e2e', fullyParallel:true, retries:process.env.CI?1:0,
 reporter:'list', use:{baseURL:'http://127.0.0.1:4173',trace:'retain-on-failure'},
 projects:[{name:'desktop-chromium',use:{...devices['Desktop Chrome']}},{name:'mobile-chromium',use:{...devices['Pixel 5']}}],
 webServer:{command:'npm run dev -- --port 4173 --strictPort',url:'http://127.0.0.1:4173',reuseExistingServer:false,timeout:30000,env:{VITE_SUPABASE_URL:'',VITE_SUPABASE_PUBLISHABLE_KEY:''}}
});
