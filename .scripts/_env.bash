#!/dev/null

set -e -E -u -o pipefail -o noclobber -o noglob +o braceexpand || exit 1
trap 'printf "[ee] failed: %s\n" "${BASH_COMMAND}" >&2' ERR || exit 1
export -n BASH_ENV

_workbench="$( readlink -e -- . )"
_scripts="${_workbench}/scripts"
_tools="${mosaic_distribution_tools:-${_workbench}/.tools}"
_outputs="${_workbench}/.outputs"
_temporary="${mosaic_distribution_temporary:-/tmp}"

_PATH="${_tools}/bin:${PATH}"

_java_bin="$( PATH="${_PATH}" type -P -- java || true )"
if test -z "${_java_bin}" ; then
	echo "[ee] missing \`java\` (Java interpreter) executable in path: \`${_PATH}\`; ignoring!" >&2
	exit 1
fi

_mvn_bin="$( PATH="${_PATH}" type -P -- mvn || true )"
if test -z "${_mvn_bin}" ; then
	echo "[ee] missing \`mvn\` (Java Maven tool) executable in path: \`${_PATH}\`; ignoring!" >&2
	exit 1
fi

_generic_env=(
		PATH="${_PATH}"
		TMPDIR="${_temporary}"
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
_mvn_args=(
		--errors
		-D_maven_pom_outputs="${_temporary}/mosaic-java-platform--$( readlink -m -- "${_workbench}/.outputs" | tr -d '\n' | md5sum -t | tr -d ' \n-' )"
)
if test -z "${_mvn_verbose:-}" ; then
	_mvn_args+=( --quiet )
fi
_mvn_env=(
		"${_generic_env[@]}"
)

while read _maven_pom_variable ; do
	test -n "${_maven_pom_variable}" || continue
	declare "${_maven_pom_variable}"
done <<<"$(
		###		--offline \
		env "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_this_pom}" \
				"${_mvn_args[@]}" \
				help:effective-pom \
				-Doutput=/dev/stderr \
			3>&1 1>&2 2>&3 \
		| grep -o -E -e '<echo message="_maven_pom_[a-z]+=.+&#xA;" file="/dev/stdout" />' \
		| sed -r -e 's!^<echo message="(_maven_pom_[a-z]+=.+)&#xA;" file="/dev/stdout" />$!\1!'
)"

_mvn_pom="${_mvn_umbrella_pom}"

test -n "${_maven_pom_artifact:-}"
test -n "${_maven_pom_version:-}"
test -n "${_maven_pom_classifier:-}"

case "${_maven_pom_classifier}" in
	( component | *-component )
		test -n "${_maven_pom_package}"
		_package_name="${_maven_pom_package}"
		_package_jar_name="${_maven_pom_artifact}-${_maven_pom_version}-${_maven_pom_classifier}.jar"
		_package_scripts=( run-component )
		_package_version="${mosaic_distribution_version:-0.6.0}"
		_package_cook="${mosaic_distribution_cook:-cook@agent1.builder.mosaic.ieat.ro}"
		_mosaic_deploy_cook="${_mosaic_deploy_cook:-true}"
		_mosaic_deploy_artifactory="${_mosaic_deploy_artifactory:-true}"
	;;
	( artifacts )
		_mosaic_deploy_cook=false
		_mosaic_deploy_artifactory="${_mosaic_deploy_artifactory:-true}"
	;;
	( * )
		false
	;;
esac
