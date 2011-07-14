#!/dev/null

if ! test "${#}" -eq 3 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

_identifier="${1:-00000000c7f30eeefd9bd86d356ceb10754aec4c}"
_class_name="${2}"
_class_path="${3}"

_jar="${_java_jars:-${_workbench}/target}/components-container-0.2-SNAPSHOT-jar-with-dependencies.jar"

_java_args+=(
		-jar "${_jar}"
		"${_class_name}"
		"${_class_path}"
)

mkdir -p "/tmp/mosaic/components/${_identifier}"
cd "/tmp/mosaic/components/${_identifier}"

exec env "${_java_env[@]}" "${_java}" "${_java_args[@]}"
