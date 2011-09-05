#!/dev/null

if ! test "${#}" -ge 1 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

_identifier="${1:-00000000c7f30eeefd9bd86d356ceb10754aec4c}"
shift 1

if test -n "${mosaic_component_temporary:-}" ; then
	_tmp="${mosaic_component_temporary:-}"
elif test -n "${mosaic_temporary:-}" ; then
	_tmp="${mosaic_temporary}/components/${_identifier}"
else
	_tmp="/tmp/mosaic/components/${_identifier}"
fi

_jar="${_java_jars:-${_workbench}/target}/${_package_jar_name}"

_java_args+=(
		-jar "${_jar}"
		"${@}"
)

mkdir -p "${_tmp}"
cd "${_tmp}"

exec env "${_java_env[@]}" "${_java}" "${_java_args[@]}"
