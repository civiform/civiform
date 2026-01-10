import fs from 'fs';
import path from 'path';

export function readJsonFile(filePath: string): any {
  const content = fs.readFileSync(filePath, 'utf-8');
  return JSON.parse(content);
}

export function getEsriFile(fileName: string, basePath: string): any {
  const filePath = path.join(basePath, fileName);
  return readJsonFile(filePath);
}
