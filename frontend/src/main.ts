import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';

dayjs.extend(customParseFormat);
(globalThis as unknown as { global: typeof globalThis }).global = globalThis;
bootstrapApplication(App, appConfig).catch((err) => console.error(err));
