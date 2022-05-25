import winston from 'winston'

export const getDebugLogger = () => {
  const logger = winsont.createLogger({
    level: 'debug',
    format: winston.format.json(),
    defaultMeta: { service: 'user-service' },
    transports: [
      new winston.transports.File({ filename: 'logs/browserTestDebug.log' }),
    ],
  })

  return logger
}
