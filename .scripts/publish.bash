#!/dev/null

if ! test "${#}" -eq 0 ; then
	echo "[ee] invalid arguments; aborting!" >&2
	exit 1
fi

if test "${pallur_publish_artifactory:-false}" == true ; then
	case "${_pom_classifier}" in
		
		( component | *-component )
			# FIXME: Add `--offline` flag, if Maven has already downloaded required plugins. (See `mosaic-distribution`.)
			env -i "${_mvn_env[@]}" "${_mvn_bin}" \
					-f "${_mvn_pom}" \
					--projects "${_pom_group}:${_pom_artifact}" \
					--also-make \
					--activate-profiles "${_mvn_profiles}" \
					"${_mvn_args[@]}" \
					deploy \
					-D_mvn_skip_all=true
		;;
		
		( artifacts )
			# FIXME: Add `--offline` flag, if Maven has already downloaded required plugins. (See `mosaic-distribution`.)
			# FIXME: We have to fix this...
			env -i "${_mvn_env[@]}" "${_mvn_bin}" \
					-f "${_mvn_pom}" \
					--also-make \
					--activate-profiles "${_mvn_profiles}" \
					"${_mvn_args[@]}" \
					deploy \
					-D_mvn_skip_all=true
			exit 0
		;;
		
		( * )
			exit 1
		;;
	esac
fi

case "${_pom_classifier}" in
		( component | *-component )
		;;
		( artifacts )
			exit 0
		;;
		( * )
			exit 1
		;;
esac

## chunk::d63e3622fd5d8efb682c6cb0faffe808::begin ##
if test "${pallur_publish_cp:-false}" == true ; then
	test -n "${pallur_publish_cp_store}"
	pallur_publish_cp_target="${pallur_publish_cp_store}/${_package_name}--${_package_version}.cpio.gz"
	echo "[ii] publishing via \`cp\` method to \`${pallur_publish_cp_target}\`..." >&2
	cp -T -- "${_outputs}/package.cpio.gz" "${pallur_publish_cp_target}"
fi
## chunk::d63e3622fd5d8efb682c6cb0faffe808::end ##

## chunk::bb510508f1a2000fc0ddbe7e8a7807d7::begin ##
if test "${pallur_publish_curl:-false}" == true ; then
	test -n "${pallur_publish_curl_credentials}"
	test -n "${pallur_publish_curl_store}"
	pallur_publish_curl_target="${pallur_publish_curl_store}/${_package_name}--${_package_version}.cpio.gz"
	echo "[ii] publishing via \`curl\` method to \`${pallur_publish_curl_target}\`..." >&2
	env -i "${_curl_env[@]}" "${_curl_bin}" "${_curl_args[@]}" \
			--anyauth --user "${pallur_publish_curl_credentials}" \
			--upload-file "${_outputs}/package.cpio.gz" \
			-- "${pallur_publish_curl_target}"
fi
## chunk::bb510508f1a2000fc0ddbe7e8a7807d7::end ##

exit 0
