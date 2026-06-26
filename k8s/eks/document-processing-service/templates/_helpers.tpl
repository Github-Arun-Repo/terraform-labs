{{- define "document-processing-service.name" -}}
document-processing-service
{{- end -}}

{{- define "document-processing-service.fullname" -}}
{{ include "document-processing-service.name" . }}
{{- end -}}

{{- define "document-processing-service.labels" -}}
app.kubernetes.io/name: {{ include "document-processing-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
