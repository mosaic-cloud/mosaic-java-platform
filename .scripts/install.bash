#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

case "${_pom_classifier}" in
	
	( component | *-component )
		# FIXME: Add `--offline` flag, if Maven has already downloaded required plugins. (See `mosaic-distribution`.)
		exec env -i "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--projects "${_pom_group}:${_pom_artifact}" \
				--also-make \
				"${_mvn_args[@]}" \
				install \
				-D_mvn_skip_all=true
	;;
	
	( artifacts )
		# FIXME: Add `--offline` flag, if Maven has already downloaded required plugins. (See `mosaic-distribution`.)
		# FIXME: We have to fix this...
		exec env -i "${_mvn_env[@]}" "${_mvn_bin}" \
				-f "${_mvn_pom}" \
				--also-make \
				"${_mvn_args[@]}" \
				install \
				-D_mvn_skip_all=true
	;;
	
	( * )
		exit 1
	;;
esac

exit 1
