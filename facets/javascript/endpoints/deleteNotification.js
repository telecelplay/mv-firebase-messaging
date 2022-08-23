const deleteNotification = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/deleteNotification/${parameters.notificationId}`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			
		})
	});
}

const deleteNotificationForm = (container) => {
	const html = `<form id='deleteNotification-form'>
		<div id='deleteNotification-notificationId-form-field'>
			<label for='notificationId'>notificationId</label>
			<input type='text' id='deleteNotification-notificationId-param' name='notificationId'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const notificationId = container.querySelector('#deleteNotification-notificationId-param');

	container.querySelector('#deleteNotification-form button').onclick = () => {
		const params = {
			notificationId : notificationId.value !== "" ? notificationId.value : undefined
		};

		deleteNotification(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { deleteNotification, deleteNotificationForm };