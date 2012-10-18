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
				--offline \
				"${_mvn_args[@]}" \
				compile test-compile
	;;
	
	( artifacts )
		# FIXME: We have to fix this...
		exec env "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--also-make \
				--offline \
				"${_mvn_args[@]}" \
				compile test-compile
	;;
	
	( * )
		exit 1
	;;
esac

exit 1
