{{- define "document-api-service.name" -}}
document-api-service
{{- end -}}

{{- define "document-api-service.fullname" -}}
{{ include "document-api-service.name" . }}
{{- end -}}

{{- define "document-api-service.labels" -}}
app.kubernetes.io/name: {{ include "document-api-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
