document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('partnerForm');
    const feedback = document.getElementById('feedback');
    const submitBtn = document.getElementById('submitBtn');
    const submitText = document.getElementById('submitText');
    const loader = document.getElementById('loader');

    // File input preview handling
    const fileInputs = ['businessRegistration', 'companyProfile'];
    fileInputs.forEach(id => {
        const input = document.getElementById(id);
        const area = input.closest('.file-upload-area');
        const status = area.querySelector('.file-status');

        input.addEventListener('change', () => {
            if (input.files && input.files[0]) {
                const file = input.files[0];
                status.textContent = `Selected: ${file.name} (${(file.size / 1024 / 1024).toFixed(2)} MB)`;
                status.classList.add('selected');
            } else {
                status.textContent = 'No file chosen';
                status.classList.remove('selected');
            }
        });

        // Drag and drop simulation
        area.addEventListener('click', () => input.click());
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        console.log('Form submission started');

        // Reset feedback
        feedback.className = '';
        feedback.innerHTML = '';
        feedback.style.display = 'none';

        // Prepare data
        const formData = new FormData();

        try {
            // Partner JSON object - pulling values robustly
            const partnerData = {
                organizationName: form.querySelector('[name="organizationName"]').value,
                legalForm: form.querySelector('[name="legalForm"]').value,
                email: form.querySelector('[name="email"]').value,
                phone: form.querySelector('[name="phone"]').value,
                website: form.querySelector('[name="website"]').value,
                city: form.querySelector('[name="city"]').value,
                address: form.querySelector('[name="address"]').value,
                partnershipType: form.querySelector('[name="partnershipType"]').value
            };

            console.log('Extracted partner data:', partnerData);

            // Client-side domain validation (Controle de saisie)
            const email = partnerData.email;
            const website = partnerData.website.toLowerCase();
            const emailParts = email.split('@');

            if (emailParts.length === 2) {
                const domain = emailParts[1].toLowerCase();
                if (!website.includes(domain)) {
                    feedback.className = 'error';
                    feedback.innerHTML = `<strong>Controle de saisie failed:</strong> Website must contain the domain name '${domain}' from your email.`;
                    feedback.style.display = 'block';
                    console.warn('Domain validation failed');
                    return;
                }
            }

            formData.append('partner', JSON.stringify(partnerData));

            const busRegFile = document.getElementById('businessRegistration').files[0];
            const compProfFile = document.getElementById('companyProfile').files[0];

            if (!busRegFile || !compProfFile) {
                throw new Error('Please select both required PDF documents.');
            }

            formData.append('businessRegistration', busRegFile);
            formData.append('companyProfile', compProfFile);

            // UI State: Loading
            submitBtn.disabled = true;
            submitText.textContent = 'Registering Partner...';
            loader.style.display = 'inline-block';

            console.log('Sending request to /api/partners...');
            const response = await fetch('/api/partners', {
                method: 'POST',
                body: formData
            });

            console.log('Response status:', response.status);

            let result;
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) {
                result = await response.json();
            } else {
                const text = await response.text();
                throw new Error(`Server returned non-JSON response: ${text.substring(0, 100)}`);
            }

            if (response.ok) {
                feedback.className = 'success';
                feedback.innerHTML = `<strong>Success!</strong> Partner registered with ID: ${result.id}`;
                feedback.style.display = 'block';
                form.reset();
                // Reset file statuses
                document.querySelectorAll('.file-status').forEach(s => {
                    s.textContent = 'No file chosen';
                    s.classList.remove('selected');
                });
                console.log('Registration successful');
            } else {
                feedback.className = 'error';
                feedback.style.display = 'block';
                let errorHtml = `<strong>Registration Failed:</strong> ${result.error || 'Request Error'}`;

                if (result.message) {
                    errorHtml += `<p>${result.message}</p>`;
                }

                if (result.fieldErrors) {
                    errorHtml += '<ul class="error-list">';
                    for (const [field, msg] of Object.entries(result.fieldErrors)) {
                        errorHtml += `<li><strong>${field}:</strong> ${msg}</li>`;
                    }
                    errorHtml += '</ul>';
                }

                feedback.innerHTML = errorHtml;
                console.error('Registration failed:', result);
            }
        } catch (error) {
            feedback.className = 'error';
            feedback.style.display = 'block';
            feedback.innerHTML = `<strong>Error:</strong> ${error.message}. <br><small>Check if the backend is running at http://localhost:8084</small>`;
            console.error('Catch Error:', error);
        } finally {
            submitBtn.disabled = false;
            submitText.textContent = 'Register Partner';
            loader.style.display = 'none';
            loadPartners(); // Refresh list to reflect state
        }
    });

    // Refresh List Logic
    const refreshBtn = document.getElementById('refreshBtn');
    const partnersContainer = document.getElementById('partnersContainer');

    async function loadPartners() {
        try {
            const response = await fetch('/api/partners?size=50');
            const data = await response.json();

            if (data.content && data.content.length > 0) {
                partnersContainer.innerHTML = data.content.map(p => `
                    <div style="background: white; padding: 1rem; border-radius: 0.75rem; border: 1px solid var(--border); box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                        <div style="display: flex; justify-content: space-between;">
                            <strong>${p.organizationName}</strong>
                            <span style="font-size: 0.75rem; color: var(--text-muted);">${p.id.substring(0, 8)}...</span>
                        </div>
                        <div style="font-size: 0.875rem; color: var(--text-muted); margin-top: 0.5rem;">
                            Email: ${p.email} | City: ${p.city} | Type: ${p.partnershipType}
                        </div>
                    </div>
                `).join('');
            } else {
                partnersContainer.innerHTML = '<p style="color: var(--text-muted); text-align: center; padding: 2rem;">No partners found in database.</p>';
            }
        } catch (error) {
            console.error('Fetch error:', error);
            partnersContainer.innerHTML = '<p style="color: var(--error); text-align: center; padding: 1rem;">Error loading partners. Check console.</p>';
        }
    }

    refreshBtn.addEventListener('click', loadPartners);
});
