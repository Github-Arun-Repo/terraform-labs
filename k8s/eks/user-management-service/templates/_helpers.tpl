{{- define "user-management-service.name" -}}
user-management-service
{{- end -}}

{{- define "user-management-service.fullname" -}}
{{ include "user-management-service.name" . }}
{{- end -}}

{{- define "user-management-service.labels" -}}
app.kubernetes.io/name: {{ include "user-management-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
