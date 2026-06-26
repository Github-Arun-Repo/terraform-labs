{{- define "document-review-service.name" -}}
document-review-service
{{- end -}}

{{- define "document-review-service.fullname" -}}
{{ include "document-review-service.name" . }}
{{- end -}}

{{- define "document-review-service.labels" -}}
app.kubernetes.io/name: {{ include "document-review-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
