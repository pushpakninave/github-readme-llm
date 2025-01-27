@import gg.jte.Content

@template.layout.page(
title = "Repository Content Generator",
content = @`
    <div class="text-center mb-12">
        <h1 class="text-4xl font-bold tracking-tight text-gray-900 mb-2">Content Generator</h1>
        <p class="text-gray-600">Generate a consolidated file from repository or local directory contents</p>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
        <div class="mb-6">
            <div class="flex space-x-4">
                <button onclick="switchTab('github')"
                        id="github-tab"
                        class="px-4 py-2 text-sm font-medium rounded-lg transition-colors duration-200 github-tab-button">
                    GitHub Repository
                </button>
                <button onclick="switchTab('local')"
                        id="local-tab"
                        class="px-4 py-2 text-sm font-medium rounded-lg transition-colors duration-200 local-tab-button">
                    Local Directory
                </button>
            </div>
        </div>

        <form hx-post="/generate" hx-target="#result-container" hx-indicator="#spinner">
            <div id="github-input" class="tab-content">
                <div class="mb-6">
                    <label for="githubUrl" class="block text-sm font-medium text-gray-700 mb-2">Repository URL</label>
                    <input
                            type="text"
                            id="githubUrl"
                            name="githubUrl"
                            class="block w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                            placeholder="https://github.com/username/repo"
                    >
                </div>
            </div>

            <div id="local-input" class="tab-content hidden">
                <div class="mb-6">
                    <label for="localPath" class="block text-sm font-medium text-gray-700 mb-2">Local Directory Path</label>
                    <input
                            type="text"
                            id="localPath"
                            name="localPath"
                            class="block w-full px-4 py-3 rounded-lg border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                            placeholder="/path/to/your/directory"
                    >
                </div>
            </div>

            <button
                    type="submit"
                    class="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-4 rounded-lg transition-colors duration-200 flex items-center justify-center"
            >
                Generate Content
            </button>
        </form>

        <div id="spinner" class="htmx-indicator flex justify-center mt-6">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>

        <div id="result-container" class="mt-6">
            <!-- Generated content will appear here -->
        </div>
    </div>

    <script>
        function switchTab(tab) {
            const githubInput = document.getElementById('github-input');
            const localInput = document.getElementById('local-input');
            const githubTab = document.getElementById('github-tab');
            const localTab = document.getElementById('local-tab');

            if (tab === 'github') {
                githubInput.classList.remove('hidden');
                localInput.classList.add('hidden');
                document.getElementById('localPath').value = '';
                githubTab.classList.add('bg-blue-600', 'text-white');
                githubTab.classList.remove('text-gray-700');
                localTab.classList.remove('bg-blue-600', 'text-white');
                localTab.classList.add('text-gray-700');
            } else {
                githubInput.classList.add('hidden');
                localInput.classList.remove('hidden');
                document.getElementById('githubUrl').value = '';
                localTab.classList.add('bg-blue-600', 'text-white');
                localTab.classList.remove('text-gray-700');
                githubTab.classList.remove('bg-blue-600', 'text-white');
                githubTab.classList.add('text-gray-700');
            }
        }

        // Initialize tabs
        document.addEventListener('DOMContentLoaded', function() {
            switchTab('github');
        });

        async function openDirectoryDialog() {
            try {
                const dirHandle = await window.showDirectoryPicker();
                const fullPath = await getFullPath(dirHandle);
                document.getElementById('localPath').value = fullPath;
            } catch (err) {
                console.error('Error selecting directory:', err);
            }
        }

        async function getFullPath(handle) {
            const segments = [];
            let current = handle;

            while (current) {
                segments.unshift(current.name);
                current = await current.queryPermission({ mode: 'read' }) === 'granted'
                    ? (await current.resolve())?.parent
                    : null;
            }

            return '/' + segments.join('/');
        }
    </script>
`
)