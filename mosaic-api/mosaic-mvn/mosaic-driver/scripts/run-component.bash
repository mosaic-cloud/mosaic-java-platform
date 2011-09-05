#!/dev/null

if ! test "${#}" -ge 1 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

_identifier="${1:-00000000c7f30eeefd9bd86d356ceb10754aec4c}"
shift 1

_jar="${_java_jars:-${_workbench}/target}/${_package_jar_name}"

_java_args+=(
		-jar "${_jar}"
		"${@}"
)

mkdir -p "/tmp/mosaic/components/${_identifier}"
cd "/tmp/mosaic/components/${_identifier}"

exec env "${_java_env[@]}" "${_java}" "${_java_args[@]}"
