#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

case "${_maven_pom_classifier}" in
	
	( component | *-component )
		exec env "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--projects "${_maven_pom_group}:${_maven_pom_artifact}" \
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
	;;
	
	( artifacts )
		# FIXME: We have to fix this...
		exec env "${_mvn_env[@]}" "${_mvn_bin}" \
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
	;;
	
	( * )
		exit 1
	;;
esac

exit 1
