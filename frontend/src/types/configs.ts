export interface Configuration {
  id: number
  configKey: string
  configValue: string
  defaultValue: string
  valueType: string
  description?: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface ConfigurationRequest {
  configKey: string
  configValue: string
  defaultValue: string
  valueType: string
  description?: string
  enabled: boolean
}
