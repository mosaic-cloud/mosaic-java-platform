#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

case "${_pom_classifier}" in
	
	( component | *-component )
		
		###		--offline \
		exec env "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--projects "${_pom_group}:${_pom_artifact}" \
				--also-make \
				"${_mvn_args[@]}" \
				compile test-compile
	;;
	
	( artifacts )
		# FIXME: We have to fix this...
		###		--offline \
		exec env "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--also-make \
				"${_mvn_args[@]}" \
				compile test-compile
	;;
	
	( * )
		exit 1
	;;
esac

exit 1
