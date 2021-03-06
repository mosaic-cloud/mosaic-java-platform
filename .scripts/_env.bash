#!/dev/null

set -e -E -u -o pipefail -o noclobber -o noglob +o braceexpand || exit 1
trap 'printf "[ee] failed: %s\n" "${BASH_COMMAND}" >&2' ERR || exit 1
export -n BASH_ENV

_workbench="$( readlink -e -- . )"
_scripts="${_workbench}/scripts"
_tools="${pallur_tools:-${_workbench}/.tools}"
_temporary="${pallur_temporary:-${pallur_TMPDIR:-${TMPDIR:-/tmp}}}"
_outputs="${_temporary}/$( basename -- "${_workbench}" )--outputs--$( readlink -e -- "${_workbench}" | tr -d '\n' | md5sum -t | tr -d ' \n-' )"

_PATH="${pallur_PATH:-${_tools}/bin:${PATH}}"
_HOME="${pallur_HOME:-${HOME}}"
_TMPDIR="${pallur_TMPDIR:-${TMPDIR:-${_temporary}}}"

if test -n "${pallur_pkg_java:-}" ; then
	_JAVA_HOME="${pallur_pkg_java}"
elif test -e "${_tools}/pkg/java" ; then
	_JAVA_HOME="${_tools}/pkg/java"
else
	_JAVA_HOME="${JAVA_HOME:-}"
fi
if test -n "${pallur_pkg_maven:-}" ; then
	_M2_HOME="${pallur_pkg_maven}"
elif test -e "${_tools}/pkg/maven" ; then
	_M2_HOME="${_tools}/pkg/maven"
else
	_M2_HOME="${M2_HOME:-}"
fi

if test -n "${_JAVA_HOME:-}" ; then
	_java_bin="${_JAVA_HOME}/bin/java"
else
	_java_bin="$( PATH="${_PATH}" type -P -- java || true )"
fi
if test -z "${_java_bin}" ; then
	echo "[ee] missing \`java\` (Java interpreter) executable in path: \`${_PATH}\`; ignoring!" >&2
	_java_bin=false
fi

if test -n "${_M2_HOME:-}" ; then
	_mvn_bin="${_M2_HOME}/bin/mvn"
else
	_mvn_bin="$( PATH="${_PATH}" type -P -- mvn || true )"
fi
if test -z "${_mvn_bin}" ; then
	echo "[ee] missing \`mvn\` (Java Maven tool) executable in path: \`${_PATH}\`; ignoring!" >&2
	_mvn_bin=false
fi

_generic_env=(
		PATH="${_JAVA_HOME}/bin:${_M2_HOME}/bin:${_PATH}"
		HOME="${_HOME}"
		TMPDIR="${_TMPDIR}"
		JAVA_HOME="${_JAVA_HOME}"
		M2_HOME="${_M2_HOME}"
)

_java_args=(
		-server
)
_java_env=(
		"${_generic_env[@]}"
)

_mvn_this_pom="${_workbench}/pom.xml"
if test -e "${_workbench}/pom-umbrella.xml" ; then
	_mvn_umbrella_pom="${_workbench}/pom-umbrella.xml"
else
	_mvn_umbrella_pom="${_workbench}/pom.xml"
fi
_mvn_profiles=use-mvn-outputs-without-target,use-mosaic-dev-repositories
_mvn_args=(
		--errors
		--batch-mode
		-D_mvn_outputs="${_outputs}"
		-D_mvn_TMPDIR="${_TMPDIR}"
		--log-file /dev/stderr
)
if test "${_mvn_debug:-false}" == true ; then
	_mvn_args+=( --debug )
elif test "${_mvn_verbose:-true}" == true ; then
	_mvn_args+=( )
else
	_mvn_args+=( --quiet )
fi
_mvn_env=(
		"${_generic_env[@]}"
)

while read _pom_variable ; do
	test -n "${_pom_variable}" || continue
	declare "${_pom_variable}"
done <<<"$(
		# FIXME: Add `--offline` flag, if Maven has already downloaded required plugins. (See `mosaic-distribution`.)
		cat -- "${_mvn_this_pom}" \
		| grep -o -E -e '^\s*<_pom_[a-z]+>.*</_pom_[a-z]+>\s*$' \
		| sed -r -e 's!^\s*<(_pom_[a-z]+)>(.*)</_pom_[a-z]+>\s*$!\1=\2!'
)"

_mvn_pom="${_mvn_umbrella_pom}"

test -n "${_pom_group:-}"
test -n "${_pom_artifact:-}"
test -n "${_pom_version:-}"
test -n "${_pom_classifier:-}"

case "${_pom_classifier}" in
	( component | *-component )
		test -n "${_pom_package}"
		_package_name="${_pom_package}"
		_package_version="${pallur_distribution_version:-0.7.0_dev}"
		_package_scripts=( run-component )
		_package_jar_name="${_pom_artifact}-${_pom_version}-${_pom_classifier}.jar"
		_artifacts_cache="${pallur_artifacts:-}"
	;;
	( artifacts )
		_package_name="${_pom_package}"
		_package_version="${pallur_distribution_version:-0.7.0_dev}"
	;;
	( * )
		false
	;;
esac
