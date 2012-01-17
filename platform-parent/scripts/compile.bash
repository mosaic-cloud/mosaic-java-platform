#!/dev/null

if test "${#}" -gt 1 ; then
	_mvn_args+=( "${@}" )
else
	_mvn_args+=( clean compile )
fi

exec env "${_mvn_env[@]}" "${_mvn_bin}" -f "${_mvn_pom}" --projects "${_maven_pom_group}:${_maven_pom_artifact}" --also-make "${_mvn_args[@]}" compile test-compile

exit 1
