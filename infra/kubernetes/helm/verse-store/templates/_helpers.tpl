{{- define "verse-store.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- define "verse-store.fullname" -}}
{{- default (printf "%s-%s" .Release.Name (include "verse-store.name" .)) .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- define "verse-store.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | quote }}
app.kubernetes.io/name: {{ include "verse-store.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
{{- define "verse-store.selectorLabels" -}}
app.kubernetes.io/name: {{ include "verse-store.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
{{- define "verse-store.secretName" -}}
{{- required "existingSecret is required; create it with create-local-secret.ps1" .Values.existingSecret }}
{{- end }}
{{- define "verse-store.image" -}}
{{- if .Values.image.digest -}}
{{ printf "%s@%s" .Values.image.repository .Values.image.digest }}
{{- else -}}
{{ printf "%s:%s" .Values.image.repository (required "image.tag is required when image.digest is empty" .Values.image.tag) }}
{{- end -}}
{{- end }}

