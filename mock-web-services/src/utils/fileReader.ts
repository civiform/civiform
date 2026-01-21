import {readFileSync} from 'node:fs';
import {join} from 'node:path';

export function readJsonFile(basePath: string, fileName: string): any {
  const filePath = join(basePath, fileName);
  const content = readFileSync(filePath, 'utf-8');
  return JSON.parse(content);
}
