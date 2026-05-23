#include "pch.h"
#include "App.xaml.h"
#include "MainWindow.xaml.h"

#if __has_include("App.g.cpp")
#include "App.g.cpp"
#endif

namespace winrt
{
    using namespace Microsoft::UI::Xaml;
}

namespace winrt::ZizuWindowsClient::implementation
{
    namespace
    {
        constexpr wchar_t kSingleInstanceMutexName[] = L"Local\\ZizuWindowsClientSingleInstance";
    }

    App::App()
    {
        InitializeComponent();
    }

    App::~App()
    {
        if (m_mutexHandle != nullptr)
        {
            ReleaseMutex(m_mutexHandle);
            CloseHandle(m_mutexHandle);
        }
    }

    void App::OnLaunched(Microsoft::UI::Xaml::LaunchActivatedEventArgs const&)
    {
        if (!acquireSingleInstance())
        {
            ExitProcess(0);
        }

        auto window = winrt::make<winrt::ZizuWindowsClient::implementation::MainWindow>();
        winrt::get_self<winrt::ZizuWindowsClient::implementation::MainWindow>(window)->InitializeWindow(hasHiddenLaunchArgument());
        m_window = window;
    }

    bool App::acquireSingleInstance()
    {
        m_mutexHandle = CreateMutexW(nullptr, TRUE, kSingleInstanceMutexName);
        if (m_mutexHandle == nullptr)
        {
            return false;
        }
        return GetLastError() != ERROR_ALREADY_EXISTS;
    }

    bool App::hasHiddenLaunchArgument() const
    {
        int argc = 0;
        LPWSTR* argv = CommandLineToArgvW(GetCommandLineW(), &argc);
        if (argv == nullptr)
        {
            return false;
        }

        bool hidden = false;
        for (int index = 1; index < argc; ++index)
        {
            if (std::wstring_view(argv[index]) == L"--hidden")
            {
                hidden = true;
                break;
            }
        }

        LocalFree(argv);
        return hidden;
    }
}
