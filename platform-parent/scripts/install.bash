#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

env "${_mvn_env[@]}" "${_mvn_bin}" \
		-f "${_mvn_pom}" \
		--also-make \
		"${_mvn_args[@]}" \
		clean \
		compile \
		package \
		install \
		-DskipTests=true \
		-D_maven_pom_skip_analyze=true \
		-D_maven_pom_skip_licenses=true \
		-D_maven_pom_skip_formatter=true

exit 0
